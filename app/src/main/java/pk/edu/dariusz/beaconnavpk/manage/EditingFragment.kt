package pk.edu.dariusz.beaconnavpk.manage

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_editing.*
import pk.edu.dariusz.beaconnavpk.R
import pk.edu.dariusz.beaconnavpk.manage.model.BeaconManaged


private const val ARG_PARAM1 = "selectedBeaconManaged"

class EditingFragment : Fragment() {

    private lateinit var beaconManaged: BeaconManaged

    //private var listener: OnFragmentInteractionListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            beaconManaged = it.getParcelable(ARG_PARAM1)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_editing, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        editedBeaconName.text = beaconManaged.locationNameAttachment.getDataDecoded()
        editedBeaconMessageContent.setText(beaconManaged.messageAttachment?.getDataDecoded())

        cancelButton.setOnClickListener { fragmentManager?.popBackStack() }

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val appCompatActivity = activity as AppCompatActivity
        appCompatActivity.supportActionBar?.title = "Editing message"
    }
    companion object {

        @JvmStatic
        fun newInstance(selectedBeacon: BeaconManaged) =
            EditingFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_PARAM1, selectedBeacon)
                }
            }
    }


    /*// TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(uri: Uri) {
        listener?.onFragmentInteraction(uri)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri)
    }*/
}
