package com.samuelfilho_dev.finance_module.launches.services;


import com.samuelfilho_dev.finance_module.launches.dtos.CreateOfxParserRequest;
import com.samuelfilho_dev.finance_module.launches.dtos.OfxResponse;

public interface OfxParserService {
    OfxResponse exec(CreateOfxParserRequest payload);
}
