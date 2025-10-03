package com.modernbank.transaction_service.rest.controller;

import com.modernbank.transaction_service.api.dto.TransactionDTO;
import com.modernbank.transaction_service.model.TransactionListModel;
import com.modernbank.transaction_service.rest.controller.api.TransactionServiceApi;
import com.modernbank.transaction_service.rest.controller.request.TransferMoneyATMRequest;
import com.modernbank.transaction_service.rest.controller.request.TransferMoneyRequest;
import com.modernbank.transaction_service.rest.controller.request.WithdrawAndDepositMoneyRequest;
import com.modernbank.transaction_service.rest.controller.request.WithdrawFromATMRequest;
import com.modernbank.transaction_service.rest.controller.response.BaseResponse;
import com.modernbank.transaction_service.rest.controller.response.GetTransactionsResponse;
import com.modernbank.transaction_service.rest.service.IMapperService;
import com.modernbank.transaction_service.rest.service.TransactionService;
import com.modernbank.transaction_service.rest.service.event.ITransactionServiceProducer;
import com.modernbank.transaction_service.rest.service.event.IWithdrawFromATMServiceProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@CrossOrigin
@RequestMapping(path = "/api/v1/transaction")
public class TransactionServiceController implements TransactionServiceApi {

    private final ITransactionServiceProducer transactionServiceProducer;

    private final IWithdrawFromATMServiceProducer withdrawFromATMServiceProducer;

    private final TransactionService transactionService;

    private final IMapperService mapperService;

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
        return null;
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
        TransactionListModel model = transactionService.getAllTransactionsByAccountId(accountId, page, size);
        List<TransactionDTO> transactionDTOs = mapperService.modelMapper(model.getTransactions(), TransactionDTO.class);

        return new GetTransactionsResponse(transactionDTOs,model.getTotalElements(),model.getTotalPages());
    } //TODO: Burayi degistirdim. Bunun front-endden gonderilmesi kismini organize et.
}