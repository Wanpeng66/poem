package com.baizhi.poem.dao;

import com.baizhi.poem.entity.Poem;

import java.util.List;

public interface PoemDAO {

     List<Poem> findAll();

    List<Poem> findByPage();

    Long findTotalCounts();
}
