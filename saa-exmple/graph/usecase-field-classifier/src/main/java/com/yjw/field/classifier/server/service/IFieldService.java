
package com.yjw.field.classifier.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yjw.field.classifier.server.entity.Field;


public interface IFieldService extends IService<Field> {
    Field getFieldByName(String name);
}
