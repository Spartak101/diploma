package searchengine.dto.parsing;

public class HttpStatusCodeError {
    private int code;
    private static String error;

    public HttpStatusCodeError(int code) {
        this.code = code;
    }

    static String httpStatusCodeError(int code) {

        switch (code) {
            case 200:
                error = "";
                break;
            case 400:
                error = "Неправильный, некорректный запрос";
                break;
            case 401:
                error = "Не авторизован (не представился)";
                break;
            case 403:
                error = "Запрещено (не уполномочен)";
                break;
            case 404:
                error = "Не найдено";
                break;
            case 408:
                error = "Истекло время ожидания";
                break;
            case 500:
                error = "Внутренняя ошибка сервера";
                break;
            case 503:
                error = "Сервис недоступен";
                break;
            default:
                error = "Неожиданный ответ сервера";
                break;
        }
        return error;
    }
}
