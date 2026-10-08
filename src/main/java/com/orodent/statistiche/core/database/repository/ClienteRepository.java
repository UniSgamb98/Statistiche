package com.orodent.statistiche.core.database.repository;

import com.orodent.statistiche.core.database.model.Cliente;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository {
    int deleteAll();
    int insertAll(List<Cliente> clienti);
    Optional<Cliente> findByCode(String codiceCliente);
    List<Cliente> findAll();
}
