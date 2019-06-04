package product.app.quickflip.ui.register

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import gllyd.co.user.Utilities.Validations
import kotlinx.android.synthetic.main.activity_signup.*
import kotlinx.android.synthetic.main.activity_signup.view.*
import kotlinx.android.synthetic.main.images_list_items.*
import product.app.quickflip.R
import product.app.quickflip.ui.BaseFragment
import product.app.quickflip.ui.verification.OtpVerificationFragment
import product.app.quickflip.utilities.Constants

class SignupFragment : BaseFragment(), View.OnClickListener {
    private var mViewModel = SignupViewModel()

    override fun onClick(p0: View?) {

        if (!internetUtil.isConnected(activity)) {
            showErrorSnackBar("No internet connection ..!!", signUpContainerLayout)
            return
        }

        if (p0 == sendOTP) {
            val validations = Validations(activity)
            if (validations.isEmpty(signUpUsername)) {
                if (validations.isEmpty(signUpPassword)) {
                    if (validations.isValidPassword(signUpPassword)) {
                        if (validations.isEmpty(signUpMobile)) {
                            if (validations.isValidNumber(signUpMobile)) {

                                val bundle = Bundle()
                                bundle.putString("from", "Signup")
                                bundle.putString("contact", signUpMobile.text.toString())
                                var frag = OtpVerificationFragment()
                                frag.arguments = bundle
                                replaceFragmentWithBackStack(R.id.containerLayout, Constants.OTP_FRAGMENT, frag)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewModel = ViewModelProviders.of(this).get(SignupViewModel::class.java)

        val inflator=contextt.getSystemService(Context.LAYOUT_INFLATER_SERVICE)

        initializeObservers()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.activity_signup, container, false)
        view.sendOTP.setOnClickListener(this)
        return view
    }

    private fun initializeObservers() {

        mViewModel.response?.observe(this, Observer {
            it?.let {
                if (it.status == 1) {
                    if (it.userInfo?.userId != null) {
                        removeFragmentFromBackStack(arrayOf(Constants.SIGNUP_FRAGMENT))
                        val bundle = Bundle()
                        bundle.putString("result", "success")
                        fragmentManager?.findFragmentByTag(Constants.LOGIN_FRAGMENT)!!.arguments = bundle
                    } else {
                        showErrorSnackBar("Something went wrong", signUpContainerLayout)
                    }
                } else {
                    showErrorSnackBar(it.message!!, signUpContainerLayout)
                }
            }
            mViewModel.isLoading.value = false
        })
        mViewModel.apiError?.observe(this, Observer {
            it?.let {
                mViewModel.isLoading.value = false
                showErrorSnackBar(it, signUpContainerLayout)
            }
        })

        mViewModel.isLoading.observe(this, Observer {
            it?.let {
                progressBar2.visibility = if (it!!) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()

        val args = arguments
        if (args != null) {
            val data = args!!.getString("result")
            if (data.equals("success")) {
                mViewModel?.doSignUp(signUpUsername.text.toString(),
                        signUpPassword.text.toString(),
                        signUpMobile.text.toString())
            }
        }
    }
}