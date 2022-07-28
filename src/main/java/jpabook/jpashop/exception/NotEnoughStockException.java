package jpabook.jpashop.exception;

public class NotEnoughStockException extends RuntimeException {

    // 일단 다 override해줘서 넣어줬다.
    public NotEnoughStockException() {
        super();
    }

    public NotEnoughStockException(String message) {
        super(message);
    }

    public NotEnoughStockException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotEnoughStockException(Throwable cause) {
        super(cause);
    }


}
