package com.act.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;



@Entity
@Table(name = "empact", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
@Data                   // Lombok: getters, setters, toString, equals, hashCode
@NoArgsConstructor      // Lombok: no-args constructor
@AllArgsConstructor     // Lombok: all-args constructor
@Builder                // Lombok: builder pattern
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "first_name")
    private String firstName;
    @Column(name = "last_name")
    private String lastName = "";
    @Column(name = "email")
    private String email;
    @Column(name = "config")
    private String config;
    @Column(name = "enable")
    private String enable;




}
