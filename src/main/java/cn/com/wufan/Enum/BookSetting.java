package cn.com.wufan.Enum;

public enum BookSetting {

    ENCODING("encoding"),
    //书名
    BOOK_NAME("bookname"),
    //作者
    AUTHOR("author")
    ;




    private String context;

    BookSetting(String context) {
        this.context = context;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }
}
