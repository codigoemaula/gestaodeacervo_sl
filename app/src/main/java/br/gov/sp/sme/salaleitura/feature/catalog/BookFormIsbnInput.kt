package br.gov.sp.sme.salaleitura.feature.catalog

/** Changing the edition identifier must never leave a previous title or author available for saving. */
internal fun BookFormState.withIsbnInput(value: String): BookFormState =
    if (isbn == value) this
    else BookFormState(isbn = value, location = location, quantity = quantity)
