package com.baizhi.poem.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.persistence.*;

@Data
@Accessors(chain = true)
@Table(name = "t_category")
@Entity
public class JpaCategory {

    @Id
    @Column(name = "id")
    private String id;


    @Basic
    @Column(name = "name")
    private String name;

}

