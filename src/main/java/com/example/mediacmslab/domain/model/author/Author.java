package com.example.mediacmslab.domain.model.author;

import java.time.LocalDateTime;

public class Author {
    private Long id;

    private String lastName;
    private String firstName;
    private String middleName;

    private String email;
    private String password;
    private String phone;

    private LocalDateTime createdAt;

    private AuthorStatus status;


}
