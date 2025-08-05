package org.bobj.notification.service;

import java.io.IOException;

public interface FcmService {
    void sendMessageTo(String targetToken, String title, String body) throws IOException;

}
