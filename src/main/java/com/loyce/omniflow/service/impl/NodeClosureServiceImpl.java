package com.loyce.omniflow.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.loyce.omniflow.dao.entity.NodeClosureDO;
import com.loyce.omniflow.dao.mapper.NodeClosureMapper;
import com.loyce.omniflow.service.NodeClosureService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NodeClosureServiceImpl extends ServiceImpl<NodeClosureMapper, NodeClosureDO> implements NodeClosureService {

}
