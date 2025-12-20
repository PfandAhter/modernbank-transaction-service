package com.modernbank.transaction_service.controller;

import com.modernbank.transaction_service.api.dto.TransactionDTO;
import com.modernbank.transaction_service.api.request.*;
import com.modernbank.transaction_service.api.TransactionServiceApi;
import com.modernbank.transaction_service.api.response.BaseResponse;
import com.modernbank.transaction_service.api.response.GetTransactionsResponse;
import com.modernbank.transaction_service.model.TransactionListModel;
import com.modernbank.transaction_service.service.MapperService;
import com.modernbank.transaction_service.service.TransactionService;
import com.modernbank.transaction_service.service.event.ITransactionServiceProducer;
import com.modernbank.transaction_service.service.event.IWithdrawFromATMServiceProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/api/v1/transaction")
public class TransactionServiceController implements TransactionServiceApi {

    private final ITransactionServiceProducer transactionServiceProducer;

    private final IWithdrawFromATMServiceProducer withdrawFromATMServiceProducer;

    private final TransactionService transactionService;

    private final MapperService mapperService;

    @Override
    public ResponseEntity<BaseResponse> withdrawMoney(WithdrawAndDepositMoneyRequest request) {
        return null;
    }

    @Override
    public ResponseEntity<BaseResponse> depositMoney(WithdrawAndDepositMoneyRequest request) {
        return null;
    }

    @Override
    public ResponseEntity<BaseResponse> transferMoney(TransferMoneyRequest request) {
        return ResponseEntity.ok(transactionServiceProducer.transferMoney(request));
    }

    @Override
    public ResponseEntity<BaseResponse> withdrawMoneyFromATM(TransferMoneyATMRequest request) {
        return ResponseEntity.ok(withdrawFromATMServiceProducer.transferMoneyATM(request));
    }

    @Override
    public ResponseEntity<BaseResponse> withdrawMoneyFromATM(WithdrawFromATMRequest request) {
        return ResponseEntity.ok(withdrawFromATMServiceProducer.withdrawMoneyFromATM(request));
    }

    @Override
    public GetTransactionsResponse getAllTransactions(String accountId, int page, int size) {
        /*TransactionListModel model = transactionService.getAllTransactionsByAccountId(accountId, page, size);
        List<TransactionDTO> transactionDTOs = mapperService.modelMapper(model.getTransactions(), TransactionDTO.class);

        return new GetTransactionsResponse(transactionDTOs,model.getTotalElements(),model.getTotalPages());*/
        return null;
    } //TODO: Burayi degistirdim. Bunun front-endden gonderilmesi kismini organize et.

    @Override
    public GetTransactionsResponse getAllTransactionsV2(GetAllTransactionsRequest request) {
        TransactionListModel model = transactionService.
                getAllTransactionsByAccountId(request);
        List<TransactionDTO> transactionDTOs = mapperService.map(model.getTransactions(), TransactionDTO.class);

        return new GetTransactionsResponse(transactionDTOs,model.getTotalElements(),model.getTotalPages());
    }

    @Override
    public BaseResponse updateTransactionInvoiceStatus(UpdateTransactionInvoiceStatus request) {
        transactionService.updateTransactionInvoiceStatus(request);
        return new BaseResponse("Transaction invoice status updated successfully.");
    }
}