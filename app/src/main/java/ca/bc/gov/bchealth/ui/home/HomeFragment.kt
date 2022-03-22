package ca.bc.gov.bchealth.ui.home

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import ca.bc.gov.bchealth.R
import ca.bc.gov.bchealth.databinding.FragmentHomeBinding
import ca.bc.gov.bchealth.ui.auth.BioMetricState
import ca.bc.gov.bchealth.ui.auth.BiometricsAuthenticationFragment
import ca.bc.gov.bchealth.ui.login.BcscAuthFragment
import ca.bc.gov.bchealth.ui.login.BcscAuthState
import ca.bc.gov.bchealth.ui.login.BcscAuthViewModel
import ca.bc.gov.bchealth.ui.login.LoginStatus
import ca.bc.gov.bchealth.utils.viewBindings
import ca.bc.gov.bchealth.viewmodel.SharedViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment(R.layout.fragment_home) {

    private val binding by viewBindings(FragmentHomeBinding::bind)
    private val viewModel: HomeViewModel by viewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private lateinit var homeAdapter: HomeAdapter
    private val bcscAuthViewModel: BcscAuthViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolBar()

        checkLogin()

        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<BioMetricState>(
            BiometricsAuthenticationFragment.BIOMETRIC_STATE
        )?.observe(viewLifecycleOwner) {
            when (it) {
                BioMetricState.SUCCESS -> {
                    viewModel.onAuthenticationRequired(false)
                    viewModel.launchCheck()
                }
                else -> {
                    findNavController().popBackStack()
                }
            }
        }

        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<BcscAuthState>(
            BcscAuthFragment.BCSC_AUTH_STATUS
        )?.observe(viewLifecycleOwner) {
            findNavController().currentBackStackEntry?.savedStateHandle?.remove<BcscAuthState>(
                BcscAuthFragment.BCSC_AUTH_STATUS
            )
            when (it) {
                BcscAuthState.SUCCESS -> {
                }
                BcscAuthState.NOT_NOW -> {
                    val destinationId = sharedViewModel.destinationId
                    if (destinationId > 0) {
                        findNavController().navigate(destinationId)
                    }
                }
                else -> {
                }
            }
        }

        viewModel.launchCheck()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    onBoardingFlow()
                }
            }
        }

        homeAdapter = HomeAdapter {
            when (it) {
                HomeNavigationType.HEALTH_RECORD -> {
                    findNavController().navigate(R.id.action_homeFragment_to_health_records)
                }
                HomeNavigationType.VACCINE_PROOF -> {
                    findNavController().navigate(R.id.action_homeFragment_to_health_pass)
                }
                HomeNavigationType.RESOURCES -> {
                    findNavController().navigate(R.id.action_homeFragment_to_resources)
                }
            }
        }
        binding.rvHome.adapter = homeAdapter
        homeAdapter.submitList(
            mutableListOf(
                HomeRecordItem(
                    R.drawable.ic_login_info,
                    "Health Records",
                    "View and manage all your available health records, including dispensed medications, health visits, COVID-19 test results, immunizations and more.",
                    R.drawable.ic_bcsc,
                    "Get Started",
                    HomeNavigationType.HEALTH_RECORD
                ),
                HomeRecordItem(
                    R.drawable.ic_green_tick,
                    "Proof of Vaccination",
                    "View, download and print your BC Vaccine Card and federal proof of vaccination, to access events, businesses, services and to travel.",
                    R.drawable.ic_right_arrow,
                    "Add proofs",
                    HomeNavigationType.VACCINE_PROOF
                ),
                HomeRecordItem(
                    R.drawable.ic_resources,
                    "Resources",
                    "Find useful information and learn how to get vaccinated or tested for COVID-19.",
                    R.drawable.ic_right_arrow,
                    "Learn more",
                    HomeNavigationType.RESOURCES
                )
            )
        )
    }

    private fun checkLogin() {
        bcscAuthViewModel.checkLogin()
        observeBcscLogin()
    }

    private fun observeBcscLogin() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                bcscAuthViewModel.authStatus.collect {
                    binding.progressBar.isVisible = it.showLoading
                    if (it.showLoading) {
                        return@collect
                    } else {
                        val isLoginStatusActive = it.loginStatus == LoginStatus.ACTIVE
                        if (isLoginStatusActive) {
                            viewModel.getPatientFirstName()
                        }
                    }
                }
            }
        }
    }

    private suspend fun onBoardingFlow() {
        viewModel.uiState.collect { uiState ->
            if (uiState.isOnBoardingRequired) {
                findNavController().navigate(R.id.onBoardingSliderFragment)
                viewModel.onBoardingShown()
            }

            if (uiState.isAuthenticationRequired) {
                findNavController().navigate(R.id.biometricsAuthenticationFragment)
            }

            if (uiState.isBcscLoginRequiredPostBiometrics) {
                findNavController().navigate(R.id.bcscAuthInfoFragment)
                viewModel.onBcscLoginRequired(false)
            }

            if(!uiState.patientFirstName.isNullOrBlank()) {
                binding.tvName.text = "Hello " + uiState.patientFirstName
            }
        }
    }

    private fun setupToolBar() {
        binding.toolbar.ivRightOption.apply {
            visibility = View.VISIBLE
            setOnClickListener {
                findNavController().navigate(R.id.profileFragment)
            }
        }
    }
}