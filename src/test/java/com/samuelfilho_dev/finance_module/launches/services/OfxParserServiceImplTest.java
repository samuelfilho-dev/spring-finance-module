package com.samuelfilho_dev.finance_module.launches.services;

import com.samuelfilho_dev.finance_module.account.entities.BankAccount;
import com.samuelfilho_dev.finance_module.account.enums.BankAccountStatus;
import com.samuelfilho_dev.finance_module.account.repositories.BankAccountRepository;
import com.samuelfilho_dev.finance_module.exceptions.ForbiddenException;
import com.samuelfilho_dev.finance_module.exceptions.NotFoundException;
import com.samuelfilho_dev.finance_module.exceptions.OfxException;
import com.samuelfilho_dev.finance_module.launches.dtos.CreateOfxParserRequest;
import com.samuelfilho_dev.finance_module.launches.entities.Launch;
import com.samuelfilho_dev.finance_module.launches.enums.LaunchCategory;
import com.samuelfilho_dev.finance_module.launches.enums.LaunchType;
import com.samuelfilho_dev.finance_module.launches.mappers.LaunchMapper;
import com.samuelfilho_dev.finance_module.launches.respositories.LaunchRepository;
import com.samuelfilho_dev.finance_module.launches.services.impl.OfxParserServiceImpl;
import com.samuelfilho_dev.finance_module.support.TestSupport;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OfxParserServiceImplTest {

    private static final String USER_ID = new ObjectId().toHexString();
    private static final String OTHER_USER_ID = new ObjectId().toHexString();
    private static final String ACCOUNT_ID = new ObjectId().toHexString();

    @Mock
    private BankAccountRepository bankAccountRepository;
    @Mock
    private LaunchRepository launchRepository;
    @Mock
    private LaunchMapper launchMapper;

    @InjectMocks
    private OfxParserServiceImpl ofxParserService;

    @BeforeEach
    void setUp() {
        TestSupport.authenticate(USER_ID);
    }

    @AfterEach
    void tearDown() {
        TestSupport.clearSecurityContext();
    }

    @Test
    void exec_shouldImportNewTransactionsAndUpdateBalance() {
        var account = ownedAccount(new BigDecimal("10.00"));
        when(bankAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
        when(launchRepository.existsByFitIdAndUserId(anyString(), anyString())).thenReturn(false);
        when(launchRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(launchMapper.toResponseList(anyList())).thenReturn(List.of());

        var result = ofxParserService.exec(request(sampleOfx()));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Launch>> captor = ArgumentCaptor.forClass(List.class);
        verify(launchRepository).saveAll(captor.capture());
        var saved = captor.getValue();

        assertEquals(2, saved.size());
        assertEquals(LaunchType.RECIPE, saved.get(0).getType());
        assertEquals(new BigDecimal("100.00"), saved.get(0).getAmount());
        assertEquals(LaunchCategory.OTHER, saved.get(0).getCategory());
        assertEquals("Salary", saved.get(0).getTitle());
        assertEquals(LaunchType.EXPENSE, saved.get(1).getType());
        assertEquals(new BigDecimal("40.00"), saved.get(1).getAmount());
        assertEquals(2, result.totalStatements());
        assertEquals(new BigDecimal("10.00"), result.oldBalance());
        verify(bankAccountRepository).save(account);
    }

    @Test
    void exec_shouldSkipTransactionsAlreadyImported() {
        var account = ownedAccount(BigDecimal.ZERO);
        when(bankAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
        when(launchRepository.existsByFitIdAndUserId("FIT001", USER_ID)).thenReturn(true);
        when(launchRepository.existsByFitIdAndUserId("FIT002", USER_ID)).thenReturn(false);
        when(launchRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(launchMapper.toResponseList(anyList())).thenReturn(List.of());

        ofxParserService.exec(request(sampleOfx()));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Launch>> captor = ArgumentCaptor.forClass(List.class);
        verify(launchRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
        assertEquals("FIT002", captor.getValue().get(0).getFitId());
    }

    @Test
    void exec_shouldThrowWhenAccountIsMissing() {
        when(bankAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> ofxParserService.exec(request(sampleOfx())));
    }

    @Test
    void exec_shouldRejectAccountOwnedByAnotherUser() {
        var account = account(OTHER_USER_ID, BigDecimal.TEN);
        when(bankAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
        assertThrows(ForbiddenException.class, () -> ofxParserService.exec(request(sampleOfx())));
        verify(launchRepository, never()).saveAll(anyList());
    }

    @Test
    void exec_shouldThrowWhenOfxTagIsMissing() {
        var account = ownedAccount(BigDecimal.TEN);
        when(bankAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

        var exception = assertThrows(OfxException.class, () -> ofxParserService.exec(request("not an ofx file")));
        assertTrue(exception.getMessage().contains("<OFX>"));
    }

    @Test
    void exec_shouldThrowWhenHeaderIsMissing() {
        var account = ownedAccount(BigDecimal.TEN);
        when(bankAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

        var exception = assertThrows(OfxException.class, () -> ofxParserService.exec(request("<OFX></OFX>")));
        assertTrue(exception.getMessage().contains("OFXHEADER"));
    }

    @Test
    void exec_shouldImportCreditCardStatement() {
        var account = ownedAccount(BigDecimal.ZERO);
        when(bankAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
        when(launchRepository.existsByFitIdAndUserId(anyString(), anyString())).thenReturn(false);
        when(launchRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(launchMapper.toResponseList(anyList())).thenReturn(List.of());

        ofxParserService.exec(request(sampleCreditCardOfx()));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Launch>> captor = ArgumentCaptor.forClass(List.class);
        verify(launchRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
        assertEquals("Card purchase", captor.getValue().get(0).getTitle());
        assertEquals(LaunchType.EXPENSE, captor.getValue().get(0).getType());
    }

    @Test
    void exec_shouldAcceptAlreadyWellFormedXml() {
        var account = ownedAccount(BigDecimal.ZERO);
        when(bankAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
        when(launchRepository.existsByFitIdAndUserId(anyString(), anyString())).thenReturn(false);
        when(launchRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(launchMapper.toResponseList(anyList())).thenReturn(List.of());

        ofxParserService.exec(request(sampleXmlOfx()));

        verify(launchRepository).saveAll(anyList());
    }

    @Test
    void exec_shouldSkipSaveWhenEveryTransactionAlreadyExists() {
        var account = ownedAccount(BigDecimal.TEN);
        when(bankAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
        when(launchRepository.existsByFitIdAndUserId(anyString(), anyString())).thenReturn(true);
        when(launchRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(launchMapper.toResponseList(anyList())).thenReturn(List.of());

        var result = ofxParserService.exec(request(sampleOfx()));

        assertEquals(0, result.totalStatements());
        assertEquals(BigDecimal.TEN, result.newBalance());
    }

    @Test
    void exec_shouldThrowWhenAmountIsInvalid() {
        var account = ownedAccount(BigDecimal.TEN);
        when(bankAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

        var exception = assertThrows(OfxException.class, () -> ofxParserService.exec(request(sampleOfxWithAmount("not-a-number"))));
        assertTrue(exception.getMessage().contains("valor monetário"));
    }

    @Test
    void exec_shouldThrowWhenDateIsInvalid() {
        var account = ownedAccount(BigDecimal.TEN);
        when(bankAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

        var exception = assertThrows(OfxException.class, () -> ofxParserService.exec(request(sampleOfxWithPostedDate("xx"))));
        assertTrue(exception.getMessage().contains("data OFX"));
    }

    @Test
    void exec_shouldThrowWhenNormalizedXmlIsMalformed() {
        var account = ownedAccount(BigDecimal.TEN);
        when(bankAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

        var exception = assertThrows(OfxException.class, () -> ofxParserService.exec(request("""
                OFXHEADER:100
                <OFX>
                <BANKMSGSRSV1>
                """)));
        assertTrue(exception.getMessage().contains("XML"));
    }

    @Test
    void exec_shouldWrapIoFailure() throws Exception {
        var account = ownedAccount(BigDecimal.TEN);
        when(bankAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
        var file = org.mockito.Mockito.mock(org.springframework.web.multipart.MultipartFile.class);
        when(file.getInputStream()).thenThrow(new IOException("disk"));

        var exception = assertThrows(OfxException.class, () -> ofxParserService.exec(new CreateOfxParserRequest(ACCOUNT_ID, file)));
        assertTrue(exception.getMessage().contains("Falha ao ler o arquivo OFX"));
    }

    private static CreateOfxParserRequest request(String content) {
        var file = new MockMultipartFile("file", "statement.ofx", "application/x-ofx", content.getBytes(StandardCharsets.UTF_8));
        return new CreateOfxParserRequest(ACCOUNT_ID, file);
    }

    private static BankAccount ownedAccount(BigDecimal balance) {
        return account(USER_ID, balance);
    }

    private static BankAccount account(String userId, BigDecimal balance) {
        return BankAccount.builder()
                .id(ACCOUNT_ID)
                .bankName("BANCO_NUBANK")
                .agency("0001")
                .accountNumber("123")
                .balance(balance)
                .status(BankAccountStatus.ACTIVE)
                .userId(new ObjectId(userId))
                .build();
    }

    private static String sampleOfx() {
        return """
                OFXHEADER:100
                DATA:OFXSGML
                VERSION:102
                SECURITY:NONE
                ENCODING:USASCII
                CHARSET:1252
                COMPRESSION:NONE
                OLDFILEUID:NONE
                NEWFILEUID:NONE
                
                <OFX>
                <BANKMSGSRSV1>
                <STMTTRNRS>
                <STMTRS>
                <CURDEF>BRL
                <BANKACCTFROM>
                <BANKID>001
                <ACCTID>12345
                <ACCTTYPE>CHECKING
                </BANKACCTFROM>
                <BANKTRANLIST>
                <DTSTART>20240101
                <DTEND>20240131
                <STMTTRN>
                <TRNTYPE>CREDIT
                <DTPOSTED>20240115
                <TRNAMT>100.00
                <FITID>FIT001
                <MEMO>Salary
                </STMTTRN>
                <STMTTRN>
                <TRNTYPE>DEBIT
                <DTPOSTED>20240116
                <TRNAMT>-40.00
                <FITID>FIT002
                <MEMO>Grocery
                </STMTTRN>
                </BANKTRANLIST>
                <LEDGERBAL>
                <BALAMT>60.00
                <DTASOF>20240131
                </LEDGERBAL>
                </STMTRS>
                </STMTTRNRS>
                </BANKMSGSRSV1>
                </OFX>
                """;
    }

    private static String sampleCreditCardOfx() {
        return """
                OFXHEADER:100
                DATA:OFXSGML
                VERSION:102
                
                <OFX>
                <CREDITCARDMSGSRSV1>
                <CCSTMTTRNRS>
                <CCSTMTRS>
                <CURDEF>BRL
                <CCACCTFROM>
                <ACCTID>9999
                </CCACCTFROM>
                <BANKTRANLIST>
                <DTSTART>20240101
                <DTEND>20240131
                <STMTTRN>
                <TRNTYPE>DEBIT
                <DTPOSTED>20240120
                <TRNAMT>-25.00
                <FITID>CC001
                <MEMO>Card purchase
                </STMTTRN>
                </BANKTRANLIST>
                <LEDGERBAL>
                <BALAMT>-25.00
                <DTASOF>20240131
                </LEDGERBAL>
                </CCSTMTRS>
                </CCSTMTTRNRS>
                </CREDITCARDMSGSRSV1>
                </OFX>
                """;
    }

    private static String sampleXmlOfx() {
        return """
                <?xml version="1.0"?>
                <OFX>
                <BANKMSGSRSV1>
                <STMTTRNRS>
                <STMTRS>
                <CURDEF>BRL</CURDEF>
                <BANKACCTFROM>
                <BANKID>001</BANKID>
                <ACCTID>12345</ACCTID>
                <ACCTTYPE>CHECKING</ACCTTYPE>
                </BANKACCTFROM>
                <BANKTRANLIST>
                <DTSTART>20240101</DTSTART>
                <DTEND>20240131</DTEND>
                <STMTTRN>
                <TRNTYPE>CREDIT</TRNTYPE>
                <DTPOSTED>20240115</DTPOSTED>
                <TRNAMT>10.00</TRNAMT>
                <FITID>XML001</FITID>
                <MEMO>Xml credit</MEMO>
                </STMTTRN>
                </BANKTRANLIST>
                <LEDGERBAL>
                <BALAMT>10.00</BALAMT>
                <DTASOF>20240131</DTASOF>
                </LEDGERBAL>
                </STMTRS>
                </STMTTRNRS>
                </BANKMSGSRSV1>
                </OFX>
                """;
    }

    private static String sampleOfxWithAmount(String amount) {
        return sampleOfx().replace("<TRNAMT>100.00", "<TRNAMT>" + amount);
    }

    private static String sampleOfxWithPostedDate(String date) {
        return sampleOfx().replace("<DTPOSTED>20240115", "<DTPOSTED>" + date);
    }
}
