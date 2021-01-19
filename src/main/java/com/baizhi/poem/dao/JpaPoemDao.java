package com.baizhi.poem.dao;

import com.baizhi.poem.entity.JpaPoem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author: wp
 * @Title: JpaPoemDao
 * @Description:
 * @date 2021/1/19 14:24
 */
@Repository
public interface JpaPoemDao extends JpaRepository<JpaPoem,String> {
}
