package br.gov.sp.sme.salaleitura.data.local

import androidx.room.TypeConverter
import br.gov.sp.sme.salaleitura.core.logic.DatabaseValueCodec
import br.gov.sp.sme.salaleitura.core.model.CopyStatus
import br.gov.sp.sme.salaleitura.core.model.PersonType
import br.gov.sp.sme.salaleitura.core.model.Shift

class Converters {
    @TypeConverter fun copyStatusToString(value: CopyStatus?): String? = value?.let(DatabaseValueCodec::copyStatusToValue)
    @TypeConverter fun stringToCopyStatus(value: String?): CopyStatus? = value?.let(DatabaseValueCodec::copyStatusFromValue)
    @TypeConverter fun personTypeToString(value: PersonType?): String? = value?.let(DatabaseValueCodec::personTypeToValue)
    @TypeConverter fun stringToPersonType(value: String?): PersonType? = value?.let(DatabaseValueCodec::personTypeFromValue)
    @TypeConverter fun shiftToString(value: Shift?): String? = value?.let(DatabaseValueCodec::shiftToValue)
    @TypeConverter fun stringToShift(value: String?): Shift? = value?.let(DatabaseValueCodec::shiftFromValue)
}
