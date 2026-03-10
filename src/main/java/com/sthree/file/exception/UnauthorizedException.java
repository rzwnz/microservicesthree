package com.sthree.file.exception;

public class UnauthorizedException extends FileServiceException {

    public UnauthorizedException(String message) {
        super(message, ErrorCode.ACCESS_DENIED, 401);
    }
}
