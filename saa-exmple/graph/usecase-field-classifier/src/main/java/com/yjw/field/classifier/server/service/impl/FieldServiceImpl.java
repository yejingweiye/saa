
package com.yjw.field.classifier.server.service.impl;

import com.yjw.field.classifier.server.entity.Field;
import com.yjw.field.classifier.server.mapper.FieldMapper;
import com.yjw.field.classifier.server.service.IFieldService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;


@Service
public class FieldServiceImpl extends ServiceImpl<FieldMapper, Field> implements IFieldService {

    @Override
    public Field getFieldByName(String name) {
        LambdaQueryWrapper<Field> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Field::getFieldName, name);
        return this.getOne(lqw);
    }
}
