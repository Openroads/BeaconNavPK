package pk.edu.dariusz.beaconnavpk.manage

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_editing.*
import org.threeten.bp.LocalDateTime
import pk.edu.dariusz.beaconnavpk.R
import pk.edu.dariusz.beaconnavpk.common.PrepareTokenAndCallTask
import pk.edu.dariusz.beaconnavpk.manage.ManageFragment.Companion.EDIT_MANAGED_BEACON_RC
import pk.edu.dariusz.beaconnavpk.manage.model.BeaconManaged
import pk.edu.dariusz.beaconnavpk.proximityapi.connectors.ProximityApiConnector
import pk.edu.dariusz.beaconnavpk.proximityapi.connectors.model.AttachmentEntry
import pk.edu.dariusz.beaconnavpk.utils.BEARER
import pk.edu.dariusz.beaconnavpk.utils.MESSAGE_TYPE
import pk.edu.dariusz.beaconnavpk.utils.PROXIMITY_NAMESPACE
import pk.edu.dariusz.beaconnavpk.utils.base64Encode
import java.lang.ref.WeakReference

/**
 * A simple [Fragment] subclass for editing beacon information in proximity API.
 * Use the [EditingFragment.newInstance] factory method to
 * create an instance of this currentFragment.
 *
 */
class EditingFragment : Fragment() {

    private val TAG = "EditingFragment"

    private lateinit var beaconManaged: BeaconManaged

    private var currentContent: String? = null

    private var disposable: Disposable? = null

    private val proximityApiConnector by lazy {
        ProximityApiConnector.create()
    }

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

        displayManagedBeacon()

        revertButton.setOnClickListener { displayManagedBeacon() }

        clearButton.setOnClickListener { editedBeaconMessageContent.setText("") }

        cancelButton.setOnClickListener { hideProgressBarAndBack() }

        deleteButton.setOnClickListener { deleteMessageAttachments() }

        saveButton.setOnClickListener { createAttachment(editedBeaconMessageContent.text.toString()) }
    }

    private fun displayManagedBeacon() {

        currentContent = beaconManaged.messageAttachment?.getDataDecoded()

        editedBeaconName.text = beaconManaged.locationNameAttachment.getDataDecoded()

        editedBeaconMessageContent.setText(currentContent?.trim())
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val appCompatActivity = activity as AppCompatActivity
        appCompatActivity.supportActionBar?.title = "Editing message"
    }

    override fun onDetach() {
        super.onDetach()
        disposable?.dispose()
    }

    private fun createAttachment(newMessageContent: String) {

        val contentToSave = if (newMessageContent.isBlank()) " " else newMessageContent.trim()

        if (currentContent != null && currentContent == contentToSave) {
            Log.i(TAG, "Saved content$contentToSave is the same as existing.")
            return
        }
        progressBarEditing.visibility = View.VISIBLE
        val newAttachment = AttachmentEntry(
            "",
            MESSAGE_NAMESPACED_TYPE,
            base64Encode(contentToSave.toByteArray()),
            LocalDateTime.now().toString() + 'Z'
        )

        PrepareTokenAndCallTask(WeakReference(requireContext())) { token ->
            val authHeader = BEARER + token

            val currentMessageAttachment = beaconManaged.messageAttachment

            disposable = proximityApiConnector.createAttachment(
                authHeader,
                beaconManaged.beaconName,
                newAttachment
            )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { response ->
                        beaconManaged.messageAttachment = response
                        deleteAttachmentAndBack(currentMessageAttachment, authHeader, beaconManaged)
                    },
                    { error ->
                        error.printStackTrace()
                        progressBarEditing.visibility = View.GONE
                        Toast.makeText(requireContext(), "Saving failure. Try again.", Toast.LENGTH_LONG).show()
                    }
                )
        }.execute(ProximityApiConnector.PROXIMITY_BEACON_SCOPE_STRING)
    }

    private fun deleteAttachmentAndBack(
        attachmentEntryToDelete: AttachmentEntry?,
        authHeader: String,
        beaconManaged: BeaconManaged
    ) {
        if (attachmentEntryToDelete == null) {
            hideProgressBarAndBack()
        } else {
            disposable = proximityApiConnector.deleteAttachment(authHeader, attachmentEntryToDelete.attachmentName)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { response ->
                        if (response.isSuccessful) {
                            Log.i(TAG, "Old attachment successfully removed..")

                            targetFragment?.onActivityResult(
                                EDIT_MANAGED_BEACON_RC,
                                RESULT_OK,
                                Intent().putExtra(UPDATED_BEACON_MANAGED, beaconManaged)
                            )
                            hideProgressBarAndBack()
                        }
                    },
                    { error ->
                        error.printStackTrace()
                        progressBarEditing.visibility = View.GONE
                        Log.e(TAG, "Old attachment removing failed.. Possible duplicates")
                    }
                )
        }
    }

    private fun deleteMessageAttachments() {
        if (beaconManaged.messageAttachment == null) {
            Toast.makeText(requireContext(), "No message to delete.", Toast.LENGTH_LONG).show()
            return
        }

        PrepareTokenAndCallTask(WeakReference(requireContext())) { token ->

            progressBarEditing.visibility = View.VISIBLE

            disposable = proximityApiConnector.deleteAttachments(
                BEARER + token,
                beaconManaged.beaconName,
                MESSAGE_NAMESPACED_TYPE
            )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { response ->

                        Log.i(TAG, "Successfully deleted: " + response.numDeleted + " attachments.")

                        progressBarEditing.visibility = View.GONE

                        Toast.makeText(requireContext(), "Successfully deleted.", Toast.LENGTH_LONG).show()

                        beaconManaged.messageAttachment = null

                        hideProgressBarAndBack()
                    },
                    { error ->
                        error.printStackTrace()
                        progressBarEditing.visibility = View.GONE
                        Toast.makeText(requireContext(), "Deleting failure. Try again.", Toast.LENGTH_LONG).show()
                    }
                )
        }.execute(ProximityApiConnector.PROXIMITY_BEACON_SCOPE_STRING)
    }

    private fun hideProgressBarAndBack() {
        progressBarEditing.visibility = View.GONE
        fragmentManager?.popBackStack()
    }

    companion object {

        val UPDATED_BEACON_MANAGED = "updatedBeaconManaged"

        private val MESSAGE_NAMESPACED_TYPE = "$PROXIMITY_NAMESPACE/$MESSAGE_TYPE"

        private const val ARG_PARAM1 = "selectedBeaconManaged"

        /**
         * Use this factory method to create a new instance of
         * this currentFragment using the provided parameters.
         *
         * @param selectedBeacon selected beacon from list provided by previous view ([ManageFragment]).
         *
         * @return A new instance of currentFragment EditingFragment.
         */
        @JvmStatic
        fun newInstance(selectedBeacon: BeaconManaged) =
            EditingFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_PARAM1, selectedBeacon)
                }
            }
    }
}
