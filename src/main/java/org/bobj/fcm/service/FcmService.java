package org.bobj.fcm.service;

import org.bobj.fcm.dto.request.FcmRequestDto;

import java.io.IOException;

public interface FcmService {
    void sendMessageTo(FcmRequestDto fcmRequestDto) throws IOException;

}
