package com.act.repo;


import com.act.model.Client;
import com.act.model.Employee;
import com.act.model.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VendorRepository extends JpaRepository<Vendor, Long> {

    Optional<Client> findByVendorName(String vendorName);

}
