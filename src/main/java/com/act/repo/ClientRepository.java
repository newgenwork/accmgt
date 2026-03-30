package com.act.repo;


import com.act.model.Client;
import com.act.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {

    Optional<Client> findByClientName(String clientName);

}
