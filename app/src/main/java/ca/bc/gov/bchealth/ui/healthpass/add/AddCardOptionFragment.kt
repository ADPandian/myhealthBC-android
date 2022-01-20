package ca.bc.gov.bchealth.ui.healthpass.add

import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import ca.bc.gov.bchealth.R
import ca.bc.gov.bchealth.databinding.FragmentAddCardOptionsBinding
import ca.bc.gov.bchealth.utils.showAlertDialog
import ca.bc.gov.bchealth.utils.showError
import ca.bc.gov.bchealth.utils.viewBindings
import ca.bc.gov.bchealth.viewmodel.AnalyticsFeatureViewModel
import ca.bc.gov.bchealth.viewmodel.SharedViewModel
import ca.bc.gov.common.model.analytics.AnalyticsAction
import ca.bc.gov.common.model.analytics.AnalyticsActionData
import ca.bc.gov.repository.model.PatientVaccineRecord
import ca.bc.gov.repository.qr.VaccineRecordState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * [AddCardOptionFragment]
 *
 * @author Pinakin Kansara
 */
@AndroidEntryPoint
class AddCardOptionFragment : Fragment(R.layout.fragment_add_card_options) {

    private val binding by viewBindings(FragmentAddCardOptionsBinding::bind)

    private val addOrUpdateCardViewModel: AddOrUpdateCardViewModel by viewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private val analyticsFeatureViewModel: AnalyticsFeatureViewModel by viewModels()

    private lateinit var action: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        action = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) {
            if (it != null) {
                addOrUpdateCardViewModel.processQRCode(it)
            }
        }

        val savedStateHandle: SavedStateHandle =
            findNavController().currentBackStackEntry!!.savedStateHandle
        savedStateHandle.getLiveData<Pair<VaccineRecordState, PatientVaccineRecord?>>(
            FetchVaccineRecordFragment.VACCINE_RECORD_ADDED_SUCCESS
        )
            .observe(
                findNavController().currentBackStackEntry!!,
                Observer {
                    if (it != null) {
                        addOrUpdateCardViewModel.processResult(it)
                    }
                }
            )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnScanQrCode.setOnClickListener {
            findNavController().navigate(R.id.action_addCardOptionFragment_to_onBoardingFragment)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {

                addOrUpdateCardViewModel.uiState.collect { state ->
                    performActionBasedOnState(state)
                }
            }
        }

        binding.btnImagePicker.setOnClickListener {
            action.launch("image/*")
        }

        binding.btnEnterInfo.setOnClickListener {
            val action = AddCardOptionFragmentDirections
                .actionAddCardOptionFragmentToFetchVaccineRecordFragment(true)
            findNavController().navigate(action)
        }

        binding.toolbar.apply {
            ivLeftOption.visibility = View.VISIBLE
            ivLeftOption.setImageResource(R.drawable.ic_action_back)
            tvTitle.visibility = View.VISIBLE
            tvTitle.text = getString(R.string.add_a_health_pass)
            ivLeftOption.setOnClickListener {
                findNavController().popBackStack()
            }
            line1.visibility = View.VISIBLE
        }
    }

    private fun performActionBasedOnState(state: AddCardOptionUiState) =
        when (state.state) {

            Status.CAN_INSERT -> {
                state.vaccineRecord?.let { insert(it) }
            }
            Status.CAN_UPDATE -> {
                state.vaccineRecord?.let { updateRecord(it) }
            }
            Status.INSERTED,
            Status.UPDATED -> {
                sharedViewModel.setModifiedRecordId(state.modifiedRecordId)
                navigateToHealthPass()
            }
            Status.DUPLICATE -> {
                requireContext().showError(
                    getString(R.string.error_duplicate_title),
                    getString(R.string.error_duplicate_message)
                )
            }
            else -> {
            }
        }

    private fun updateRecord(vaccineRecord: PatientVaccineRecord) {
        requireContext().showAlertDialog(
            title = getString(R.string.replace_health_pass_title),
            message = getString(R.string.replace_health_pass_message),
            positiveButtonText = getString(R.string.replace),
            negativeButtonText = getString(R.string.not_now)
        ) {
            addOrUpdateCardViewModel.update(vaccineRecord)
        }
    }

    private fun insert(vaccineRecord: PatientVaccineRecord) {
        addOrUpdateCardViewModel.insert(vaccineRecord)
    }

    private fun navigateToHealthPass() {
        // Snowplow event
        analyticsFeatureViewModel.track(AnalyticsAction.ADD_QR, AnalyticsActionData.UPLOAD)
        findNavController().navigate(R.id.action_addCardOptionFragment_to_healthPassFragment)
    }
}
