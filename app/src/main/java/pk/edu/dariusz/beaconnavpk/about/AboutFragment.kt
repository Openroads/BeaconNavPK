package pk.edu.dariusz.beaconnavpk.about

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import pk.edu.dariusz.beaconnavpk.R
import pk.edu.dariusz.beaconnavpk.common.IdentifiableElement

// the currentFragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Fragment displays for user information about application and legend for navigation.
 *
 * Use the [AboutFragment.newInstance] factory method to
 * create an instance of this currentFragment.
 *
 */
class AboutFragment : Fragment(), IdentifiableElement {

    private val TAG = "AboutFragment_TAG"

    override fun getIdentifier(): String {
        return TAG
    }

    // TODO: to clean up if not used
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this currentFragment
        return inflater.inflate(R.layout.fragment_about, container, false)
    }

    /*fun onButtonPressed(uri: Uri) {
        listener?.onFragmentInteraction(uri)
    }*/

    /*override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }*/

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        val appCompatActivity = activity as AppCompatActivity
        appCompatActivity.supportActionBar?.title = "About application"
        super.onActivityCreated(savedInstanceState)
    }

    /*override fun onDetach() {
        super.onDetach()
        listener = null
    }*/

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this currentFragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of currentFragment AboutFragment.
         */
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            AboutFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
