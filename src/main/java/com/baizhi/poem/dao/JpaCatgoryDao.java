package com.baizhi.poem.dao;

import com.baizhi.poem.entity.JpaCategory;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author: wp
 * @Title: JpaCatgoryDao
 * @Description:
 * @date 2021/1/19 14:16
 */
public interface JpaCatgoryDao extends JpaRepository<JpaCategory,String> {
}
