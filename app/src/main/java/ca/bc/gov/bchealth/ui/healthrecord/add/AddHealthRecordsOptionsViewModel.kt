package ca.bc.gov.bchealth.ui.healthrecord.add

import androidx.lifecycle.ViewModel
import ca.bc.gov.bchealth.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * @author Pinakin Kansara
 */
@HiltViewModel
class AddHealthRecordsOptionsViewModel @Inject constructor() : ViewModel() {

    fun getHealthRecordOption(): List<HealthRecordOption> = listOf(
        HealthRecordOption(
            R.string.get_vaccination_records,
            R.string.access_to_the_details_of_you_and_your_family_s_covid_19_vaccination_records,
            R.drawable.ic_health_record_add_vaccine,
            OptionType.VACCINE
        ),
        HealthRecordOption(
            R.string.get_covid_19_test_results,
            R.string.access_to_the_details_of_you_and_your_family_s_covid_19_test_results,
            R.drawable.ic_covid_test_result,
            OptionType.TEST
        )
    )
}

data class HealthRecordOption(
    val titleStringResource: Int,
    val descriptionStringResource: Int,
    val iconDrawableResource: Int,
    val type: OptionType
)

enum class OptionType {
    VACCINE,
    TEST
}
