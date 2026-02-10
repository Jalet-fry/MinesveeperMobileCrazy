package app.crazy_minesveeper.domain.model

enum class GameEvent {
    VOID_CLICK_R,    // Клик правой кнопкой по пустоте
    FLAG_OR_UNFLAG,  // Постановка/снятие флага
    VOID_CLICK_L,    // Клик левой кнопкой по пустоте
    DIG_OR_CHORD,    // Открытие клетки или аккорд
    START_GAME       // Начало игры (первый клик)
}
