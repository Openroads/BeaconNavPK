package pk.edu.dariusz.beaconnavpk

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentTransaction
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_navigation_drawer.*
import kotlinx.android.synthetic.main.app_bar_navigation_drawer.*
import pk.edu.dariusz.beaconnavpk.model.IdentifiableElement

class NavigationActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
    AboutFragment.OnFragmentInteractionListener {

    //private lateinit var accountSharedPref: SharedPreferences
    private var googleSignInAccount: GoogleSignInAccount? = null

    override fun onFragmentInteraction(uri: Uri) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
            this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
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
                val ft: FragmentTransaction = supportFragmentManager.beginTransaction()
                ft.replace(R.id.content_frame, newFragment as Fragment)
                ft.commit()
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

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.navigation_drawer, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            /*if activity has back button
            android.R.id.home -> {
                    finish()
                    true
                }*/
            R.id.action_settings -> {
                item.isChecked = !item.isChecked
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        displaySelectedScreen(item.itemId)
        return true
    }

    private val TAG = "NavigationActivity_TAG"

}
