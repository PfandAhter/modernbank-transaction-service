package com.modernbank.transaction_service.constant;

public class ErrorCodeConstants {

    public static final String SUCCESS = "H0000";

    public static final String FAILED = "H0001";

    public static final String INSUFFICIENT_FUNDS = "H0002";

    public static final String RECEIVER_ACCOUNT_NOT_MATCH = "H0003";

    public static final String ATM_TRANSFER_NOT_FOUND_BY_IBAN_OR_ATMID = "H0004";

    public static final String ATM_TRANSFER_NOT_FOUND_BY_IBAN = "H0005";

    public static final String ATM_TRANSFER_NOT_FOUND_BY_TCKN_OR_ATMID = "H0006";

    public static final String ATM_TRANSFER_NOT_FOUND_BY_TCKN = "H0007";

    public static final String CURRENCY_IS_NOT_SAME = "H0008";

    public static final String ACCOUNT_NAME_IS_NOT_SAME = "H0009";

    public static final String ACCOUNT_NOT_FOUND = "H0010";

    public static final String BRANCH_NOT_FOUND = "H0011";

    public static final String USER_NOT_FOUND = "H0012";

    public static final String ACCOUNT_NOT_FOUND_BY_IBAN = "H0013";

    public static final String PDF_GET_FAILED_OR_NOT_FOUND = "H0014";

    public static final String ERROR_OCCURRED_WHILE_PDF_ENCRYPTION = "H0015";

    public static final String SECURITY_ERROR_OCCURRED_WHILE_PDF_ENCRYPTION = "H0016";

    public static final String INVALID_ENCRYPTED_DATA_FORMAT = "H0018";

    public static final String ERROR_OCCURRED_WHILE_PDF_DECRYPTION = "H0019";

    public static final String PDF_ENCRYPT_PASSWORD_NOT_VALID = "H0020";

    public static final String ERROR_OCCURRED_WHILE_GENERATING_PDF_REPORT = "H0021";

    public static final String INVALID_PDF_TYPE = "H0022";

    public static final String STRATEGY_NOT_FOUND = "H0023";

    public static final String ATM_CREATION_FAILED = "H0024";

    public static final String ATM_NOT_FOUND_BY_ATMID = "H0025";

    public static final String ATM_SUPPORTED_BANK_ADD_PROCESS_FAILED = "H0026";

    public static final String BANK_CREATION_FAILED = "H0027";

    public static final String BANK_NOT_FOUND = "H0028";

    public static final String PDF_GENERATION_FAILED = "H0029";





    public static final String TECH_TRANSFER_ERROR = "TECH_ERR_TRANSFER_FLOW";

    public static final String TECH_DEPOSIT_ERROR = "TECH_ERR_DEPOSIT_FLOW";

    public static final String TECH_START_TRANSFER_MONEY_ERROR = "TECH_ERR_START_TRANSFER_MONEY";

    public static final String TECH_UPDATE_TRANSFER_MONEY_ERROR = "TECH_ERR_UPDATE_TRANSFER_MONEY";

    public static final String TECH_FINALIZE_TRANSFER_MONEY_ERRORR = "TECH_ERR_FINALIZE_TRANSFER_MONEY";
}
