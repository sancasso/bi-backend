package com.canso.csbi.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.canso.csbi.model.entity.Chart;
import com.canso.csbi.service.ChartService;
import com.canso.csbi.mapper.ChartMapper;
import org.springframework.stereotype.Service;

/**
* @author zcs
* @description 针对表【chart(图表信息表)】的数据库操作Service实现
* @createDate 2023-06-03 09:47:42
*/
@Service
public class ChartServiceImpl extends ServiceImpl<ChartMapper, Chart>
    implements ChartService{

}




