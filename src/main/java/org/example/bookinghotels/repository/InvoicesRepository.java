package org.example.bookinghotels.repository;

import org.example.bookinghotels.entity.Invoices;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoicesRepository extends JpaRepository<Invoices, Integer> {}