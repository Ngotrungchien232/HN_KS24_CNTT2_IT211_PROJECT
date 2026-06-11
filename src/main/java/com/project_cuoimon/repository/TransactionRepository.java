package com.project_cuoimon.repository;

import com.project_cuoimon.entity.Account;
import com.project_cuoimon.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    // Câu lệnh JPQL sử dụng phép toán OR để quét tất cả các giao dịch mà tài khoản đóng vai trò gửi hoặc nhận (UC-06)
    @Query("SELECT t FROM Transaction t WHERE t.fromAccount = :account OR t.toAccount = :account ORDER BY t.createdAt DESC")
    Page<Transaction> findByAccountFromOrTo(Account account, Pageable pageable);
}
