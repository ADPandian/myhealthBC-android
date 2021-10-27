package ca.bc.gov.bchealth.ui.addcard

import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import ca.bc.gov.bchealth.R
import ca.bc.gov.bchealth.databinding.FragmentAddCardOptionsBinding
import ca.bc.gov.bchealth.utils.Response
import ca.bc.gov.bchealth.utils.viewBindings
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * [AddCardOptionFragment]
 *
 * @author Pinakin Kansara
 */
@AndroidEntryPoint
class AddCardOptionFragment : Fragment(R.layout.fragment_add_card_options) {

    private val binding by viewBindings(FragmentAddCardOptionsBinding::bind)

    private val viewModel: AddCardOptionViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnScanQrCode.setOnClickListener {
            findNavController().navigate(R.id.action_addCardOptionFragment_to_onBoardingFragment)
        }

        val action = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) {
            if (it != null)
                viewModel.processUploadedImage(it, requireContext())
        }

        binding.btnImagePicker.setOnClickListener {
            action.launch("image/*")

            viewLifecycleOwner.lifecycleScope.launch {
                lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                    viewModel.responseSharedFlow.collect {
                        when (it) {
                            is Response.Success -> {
                                findNavController().popBackStack(R.id.myCardsFragment, false)
                            }
                            is Response.Error -> {
                                showError(
                                    it.errorData?.errorTitle.toString(),
                                    it.errorData?.errorMessage.toString()
                                )
                            }
                            is Response.Loading -> {
                            }
                        }
                        this.cancel()
                    }
                }
            }
        }

        binding.btnGetCard.setOnClickListener {
            findNavController()
                .navigate(R.id.action_addCardOptionFragment_to_fetchVaccineCardFragment)
        }

        binding.toolbar.apply {
            ivBack.visibility = View.VISIBLE
            ivBack.setImageResource(R.drawable.ic_action_back)
            tvTitle.visibility = View.VISIBLE
            tvTitle.text = getString(R.string.add_a_bc_vaccine_card)
            ivBack.setOnClickListener {
                findNavController().popBackStack()
            }
            line1.visibility = View.VISIBLE
        }
    }

    private fun showError(title: String, message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setCancelable(false)
            .setMessage(message)
            .setPositiveButton(getString(android.R.string.ok)) { dialog, which ->
                dialog.dismiss()
            }
            .show()
    }
}
