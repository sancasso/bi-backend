package com.canso.csbi.manager;


import com.canso.csbi.common.ErrorCode;
import com.canso.csbi.exception.BusinessException;
import com.yupi.yucongming.dev.client.YuCongMingClient;

import com.yupi.yucongming.dev.common.BaseResponse;
import com.yupi.yucongming.dev.model.DevChatRequest;
import com.yupi.yucongming.dev.model.DevChatResponse;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class AiManager {

    @Resource
    private YuCongMingClient yuCongMingClient;

    public String doChat(long modelId, String message) {
        DevChatRequest devChatRequest = new DevChatRequest();
        devChatRequest.setModelId(modelId);
        devChatRequest.setMessage(message);

//        String accessKey = "8i7zegqu3927cu8tyezz2lrsepsxiubo";
//        String secretKey = "b5ga7986v90i01fxt3eo9lhmetr6z5xu";
//        yuCongMingClient = new YuCongMingClient(accessKey, secretKey);

        BaseResponse<DevChatResponse> response = yuCongMingClient.doChat(devChatRequest);
        if (response == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 响应错误");
        }
        return response.getData().getContent( );
    }
}
