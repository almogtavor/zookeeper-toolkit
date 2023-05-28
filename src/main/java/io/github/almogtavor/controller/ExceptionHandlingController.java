package io.github.almogtavor.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.solr.client.solrj.SolrServerException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
class ExceptionHandlingController {
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String serverExceptionHandler(Exception ex) {
        log.error(ex.getMessage(), ex);
        return ex.getMessage();
    }

//    @ExceptionHandler(DuplicateKeyException.class)
//    public ResponseEntity<ErrorResponse> handleDuplicateKeyException(DuplicateKeyException ex) {
//        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("A Document with the same text already exists"));
//    }

    @ExceptionHandler(SolrServerException.class)
    public ResponseEntity<SolrServerException> handleDocumentNotFoundException(SolrServerException ex) {
        return ResponseEntity.notFound().build();
    }

}
