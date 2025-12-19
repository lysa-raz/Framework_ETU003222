package com.itu.ETU003222.model;

public class FileUpload {
    private String fileName;
    private String contentType;
    private byte[] fileContent;
    private long fileSize;
    
    public FileUpload() {}
    
    public FileUpload(String fileName, String contentType, byte[] fileContent, long fileSize) {
        this.fileName = fileName;
        this.contentType = contentType;
        this.fileContent = fileContent;
        this.fileSize = fileSize;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public String getContentType() {
        return contentType;
    }
    
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    
    public byte[] getFileContent() {
        return fileContent;
    }
    
    public void setFileContent(byte[] fileContent) {
        this.fileContent = fileContent;
    }
    
    public long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
}