package com.samuelfilho_dev.finance_module.launches.services.impl;

import com.samuelfilho_dev.finance_module.account.repositories.BankAccountRepository;
import com.samuelfilho_dev.finance_module.exceptions.OfxException;
import com.samuelfilho_dev.finance_module.launches.dtos.CreateOfxParserRequest;
import com.samuelfilho_dev.finance_module.launches.dtos.OfxResponse;
import com.samuelfilho_dev.finance_module.launches.entities.Launch;
import com.samuelfilho_dev.finance_module.launches.enums.AccountOfxType;
import com.samuelfilho_dev.finance_module.launches.enums.LaunchType;
import com.samuelfilho_dev.finance_module.launches.mappers.LaunchMapper;
import com.samuelfilho_dev.finance_module.launches.ofx.OfxAccount;
import com.samuelfilho_dev.finance_module.launches.ofx.OfxBalance;
import com.samuelfilho_dev.finance_module.launches.ofx.OfxStatement;
import com.samuelfilho_dev.finance_module.launches.ofx.OfxTransaction;
import com.samuelfilho_dev.finance_module.launches.respositories.LaunchRepository;
import com.samuelfilho_dev.finance_module.launches.services.OfxParserService;
import com.samuelfilho_dev.finance_module.launches.utils.LaunchUtils;
import com.samuelfilho_dev.finance_module.launches.utils.SgmlToXmlConverter;
import com.samuelfilho_dev.finance_module.users.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.springframework.stereotype.Service;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

@Service
@Slf4j
@RequiredArgsConstructor
public class OfxParserServiceImpl implements OfxParserService {
    public static final DateTimeFormatter OFX_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final UserRepository userRepository;
    private final BankAccountRepository bankAccountRepository;
    private final LaunchRepository launchRepository;

    private final LaunchMapper launchMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OfxResponse exec(CreateOfxParserRequest payload) {
        try {
            log.info("Iniciando processamento do arquivo OFX para o usuário: {}, conta bancária: {}", payload.userId(), payload.bankAccountId());

            this.userRepository.findById(payload.userId())
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado: " + payload.userId()));

            var bankAccount = this.bankAccountRepository.findById(payload.bankAccountId())
                    .orElseThrow(() -> new RuntimeException("Conta bancária não encontrada: " + payload.bankAccountId()));

            var oldBalance = bankAccount.getBalance();

            var statements = parse(payload.file().getInputStream());
            var launches = statements.stream()
                    .flatMap(statement -> statement.transactions().stream()
                            .map(transaction -> Launch.builder()
                                    .userId(new ObjectId(payload.userId()))
                                    .bankAccountId(new ObjectId(payload.bankAccountId()))
                                    .title(transaction.memo())
                                    .description(null)
                                    .launchDate(transaction.datePosted().atStartOfDay(ZoneOffset.UTC).toInstant())
                                    .amount(transaction.amount().abs())
                                    .fitId(transaction.fitId())
                                    .type(transaction.amount().compareTo(BigDecimal.ZERO) > 0
                                            ? LaunchType.RECIPE
                                            : LaunchType.EXPENSE
                                    )
                                    .build()))
                    .filter(launch -> !this.launchRepository.existsByFitId(launch.getFitId()))
                    .toList();

            var salvedLaunches = this.launchRepository.saveAll(launches);

            log.info("Processamento do arquivo OFX concluído. {} lançamentos salvos para o usuário: {}, conta bancária: {}", salvedLaunches.size(), payload.userId(), payload.bankAccountId());

            var newBalance = LaunchUtils.calculateNewBalanceWithLaunches(salvedLaunches, oldBalance);

            bankAccount.setBalance(newBalance);
            this.bankAccountRepository.save(bankAccount);

            log.info("Saldo atualizado para a conta bancária: {}. Saldo antigo: {}, Saldo novo: {}", payload.bankAccountId(), oldBalance, newBalance);

            return new OfxResponse(
                    launchMapper.toResponseList(salvedLaunches),
                    salvedLaunches.size(),
                    oldBalance,
                    newBalance
            );
        } catch (IOException e) {
            log.error("Falha ao ler o arquivo OFX: {}", e.getMessage(), e);
            throw new OfxException("Falha ao ler o arquivo OFX: " + e.getMessage(), e);
        }
    }

    private List<OfxStatement> parse(InputStream input) {
        var raw = readFile(input);
        var xml = toWellFormedXml(raw);
        var doc = parseXml(xml);

        return Stream.concat(
                elements(doc.getElementsByTagName("STMTRS")).map(this::parseBankStatement),
                elements(doc.getElementsByTagName("CCSTMTRS")).map(this::parseCreditCardStatement)
        ).toList();
    }

    private OfxStatement parseCreditCardStatement(Element stmtRs) {
        var ccAcctFrom = firstElement(stmtRs, "CCACCTFROM");
        var account = new OfxAccount(
                AccountOfxType.CREDIT_CARD,
                null,
                text(ccAcctFrom, "ACCTID"),
                "CREDITCARD"
        );

        return buildStatement(stmtRs, account);
    }

    private OfxStatement parseBankStatement(Element stmtRs) {
        var bankAcctFrom = firstElement(stmtRs, "BANKACCTFROM");
        var account = new OfxAccount(
                AccountOfxType.BANK,
                text(bankAcctFrom, "BANKID"),
                text(bankAcctFrom, "ACCTID"),
                text(bankAcctFrom, "ACCTTYPE")
        );
        return buildStatement(stmtRs, account);
    }

    private OfxStatement buildStatement(Element stmtRs, OfxAccount account) {
        var currency = text(stmtRs, "CURDEF");

        var tranList = firstElement(stmtRs, "BANKTRANLIST");
        var periodStart = Optional.ofNullable(tranList)
                .map(e -> text(e, " DTSTART"))
                .map(this::parseDate)
                .orElse(null);

        var periodEnd = Optional.ofNullable(tranList)
                .map(e -> text(e, "DTEND"))
                .map(this::parseDate)
                .orElse(null);

        List<OfxTransaction> transactions = tranList == null
                ? List.of()
                : elements(tranList.getElementsByTagName("STMTTRN"))
                .map(this::parseTransaction)
                .toList();

        var ledger = toBalance(firstElement(stmtRs, "LEDGERBAL"));
        var available = toBalance(firstElement(stmtRs, "AVAILBAL"));

        return new OfxStatement(
                account, currency, periodStart, periodEnd, transactions, ledger, available
        );
    }

    private Document parseXml(String xml) {
        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            var builder = factory.newDocumentBuilder();

            return builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new OfxException("Falha ao interpretar o XML normalizado do OFX: " + e.getMessage(), e);
        }
    }

    private String toWellFormedXml(String raw) {
        var content = raw.strip();

        if (content.startsWith("<?xml")) {
            return content;
        }

        var ofxTagStart = content.indexOf("<OFX>");
        if (ofxTagStart < 0) {
            throw new OfxException("Arquivo OFX inválido: tag <OFX> não encontrada.");
        }

        var header = content.substring(0, ofxTagStart);
        if (!header.contains("OFXHEADER")) {
            throw new OfxException("Cabeçalho OFX (OFXHEADER) ausente ou inválido.");
        }

        var body = content.substring(ofxTagStart + "<OFX>".length());
        return "<OFX>" + SgmlToXmlConverter.convert(body);
    }

    private String readFile(InputStream in) {
        try {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new OfxException("Error reading input stream", e);
        }
    }

    private Stream<Element> elements(NodeList node) {
        return IntStream.range(0, node.getLength())
                .mapToObj(node::item)
                .map(Element.class::cast);
    }

    private Element firstElement(Node parent, String tag) {
        var list = (parent instanceof Document doc)
                ? doc.getElementsByTagName(tag)
                : ((Element) parent).getElementsByTagName(tag);
        return list.getLength() > 0 ? (Element) list.item(0) : null;
    }

    private OfxBalance toBalance(Element el) {
        if (el == null) return null;

        var amount = parseAmount(text(el, "BALAMT"));
        var date = parseDate(text(el, "DTASOF"));
        return new OfxBalance(amount, date);
    }

    private OfxTransaction parseTransaction(Element trnRs) {
        return new OfxTransaction(
                text(trnRs, "TRNTYPE"),
                parseDate(text(trnRs, "DTPOSTED")),
                parseAmount(text(trnRs, "TRNAMT")),
                text(trnRs, "FITID"),
                text(trnRs, "CHECKNUM"),
                text(trnRs, "NAME"),
                text(trnRs, "MEMO")
        );
    }

    private BigDecimal parseAmount(String raw) {
        if (raw == null || raw.isBlank()) return null;

        try {
            return new BigDecimal(raw.replace(",", "."));
        } catch (NumberFormatException e) {
            throw new OfxException("Falha ao interpretar o valor monetário OFX: " + raw, e);
        }
    }

    private LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) return null;

        var digits = raw.length() >= 8 ? raw.substring(0, 8) : raw;

        try {
            return LocalDate.parse(digits, OFX_DATE);
        } catch (Exception e) {
            throw new OfxException("Falha ao interpretar a data OFX: " + raw, e);
        }
    }

    private String text(Element parent, String tag) {
        if (parent == null) return null;

        var list = parent.getElementsByTagName(tag);
        if (list.getLength() == 0) return null;

        var value = list.item(0).getTextContent();
        return value == null ? null : value.strip();
    }
}
