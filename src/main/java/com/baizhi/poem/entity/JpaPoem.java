package com.baizhi.poem.entity;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import javax.persistence.*;

@Data
@Accessors(chain = true)
@Table(name = "t_poem")
@Entity
public class JpaPoem {

    @Id
    @Column(name = "id")
    private String id;

    @Basic
    @Column(name = "name")
    private String name;

    @Basic
    @Column(name = "author")
    private String author;

    @Basic
    @Column(name = "type")
    private String type;

    @Basic
    @Column(name = "content")
    private String content;

    @Basic
    @Column(name = "href")
    private String href;

    @Basic
    @Column(name = "authordes")
    private String authordes;

    @Basic
    @Column(name = "origin")
    private String origin;

    @Basic
    @Column(name = "categoryId")
    private String categoryId;



}
