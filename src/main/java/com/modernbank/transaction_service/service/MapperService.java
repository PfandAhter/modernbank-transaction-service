package com.modernbank.transaction_service.service;

import java.util.List;

public interface MapperService {

    <T,D> List<D> map(List<T> source, Class<D> destination);

    <D> D map(Object source, Class<D> destinationType);

    void map(Object source, Object destination);
}
