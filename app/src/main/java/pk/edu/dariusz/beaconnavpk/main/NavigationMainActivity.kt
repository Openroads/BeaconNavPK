package pk.edu.dariusz.beaconnavpk.main

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentTransaction
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_navigation_drawer.*
import kotlinx.android.synthetic.main.app_bar_navigation_drawer.*
import pk.edu.dariusz.beaconnavpk.R
import pk.edu.dariusz.beaconnavpk.about.AboutFragment
import pk.edu.dariusz.beaconnavpk.common.IdentifiableElement
import pk.edu.dariusz.beaconnavpk.manage.EditingFragment
import pk.edu.dariusz.beaconnavpk.manage.ManageFragment
import pk.edu.dariusz.beaconnavpk.manage.model.BeaconManaged
import pk.edu.dariusz.beaconnavpk.navigation.NavigateFragment

class NavigationMainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
    ManageFragment.OnListFragmentInteractionListener {

    //private lateinit var accountSharedPref: SharedPreferences
    private var googleSignInAccount: GoogleSignInAccount? = null

    override fun onBeaconManageListFragmentInteraction(selectedItem: BeaconManaged?) {
        println("Item: $selectedItem")
        if (selectedItem != null) {
            val editingFragment = EditingFragment.newInstance(selectedItem)
            editingFragment.setTargetFragment(currentFragment as Fragment, 112)
            supportFragmentManager.beginTransaction()
                .replace(R.id.content_frame, editingFragment)
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation_drawer)
        googleSignInAccount = GoogleSignIn.getLastSignedInAccount(this)

        val editPermission = intent.extras?.getBoolean(WelcomeActivity.IS_EDITOR_KEY)

        if (editPermission != null) {
            nav_view.menu.findItem(R.id.nav_management_section).isVisible = editPermission
        }

        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(
            this, drawer_layout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)

//old way (probably to remove)
//        accountSharedPref = getSharedPreferences(PREFERENCE_ACCOUNT, Context.MODE_PRIVATE)
//        val accName = accountSharedPref.getString(AccountManager.KEY_ACCOUNT_NAME, "")
        googleSignInAccount?.let { gSignInAcc ->
            val accName = gSignInAcc.displayName
            if (!accName.isNullOrBlank()) {
                nav_view.getHeaderView(0).findViewById<TextView>(R.id.accountName).text = accName
            }

            gSignInAcc.email?.let { accEmail ->
                nav_view.getHeaderView(0).findViewById<TextView>(R.id.accountEmail).text = accEmail
            }

            gSignInAcc.photoUrl?.let { pUrl ->
                val userPhotoImageView = nav_view.getHeaderView(0).findViewById<ImageView>(R.id.userPhotoImageView)
                Picasso.with(this).load(pUrl).resize(250, 250).into(userPhotoImageView)
            }
        }

        displaySelectedScreen(R.id.nav_navigation)
    }

    private var currentFragment: IdentifiableElement? = null
    private fun displaySelectedScreen(itemId: Int) {

        var newFragment: IdentifiableElement? = null
        //creating fragment object
        when (itemId) {
            R.id.nav_navigation -> {
                newFragment = NavigateFragment.newInstance()
            }
            R.id.nav_info -> {
                newFragment = AboutFragment.newInstance("aaa", "info")
            }
            R.id.nav_manage -> {
                newFragment = ManageFragment.newInstance(1)
            }
            R.id.nav_sign_out -> {
                val intent = Intent(this, WelcomeActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                intent.putExtra(WelcomeActivity.SIGNING_OUT_KEY, true)
                startActivity(intent)
            }
        }

        //replacing the currentFragment
        if (newFragment != null) {
            if (currentFragment == null || currentFragment!!.getIdentifier() != newFragment.getIdentifier()) {
                Log.i(TAG, "Navigation to " + newFragment.getIdentifier())
                val ft: FragmentTransaction = supportFragmentManager.beginTransaction()
                ft.replace(R.id.content_frame, newFragment as Fragment)
                    .commit()
                currentFragment = newFragment
            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.navigation_drawer, menu)
        return true
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        displaySelectedScreen(item.itemId)
        return true
    }

    private val TAG = "NavigationActivity_TAG"
}
