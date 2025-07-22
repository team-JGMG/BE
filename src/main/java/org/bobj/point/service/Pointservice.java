package org.bobj.point.service;


import lombok.RequiredArgsConstructor;
import org.bobj.point.repository.PointRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class Pointservice {
    private final PointRepository pointRepository;



}
