package pk.edu.dariusz.beaconnavpk

import android.accounts.AccountManager
import android.content.Context
import android.content.SharedPreferences
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
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_navigation_drawer.*
import kotlinx.android.synthetic.main.app_bar_navigation_drawer.*
import pk.edu.dariusz.beaconnavpk.model.IdentifiableElement
import pk.edu.dariusz.beaconnavpk.utils.PREFERENCE_ACCOUNT

class NavigationActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
    AboutFragment.OnFragmentInteractionListener {

    private lateinit var accountSharedPref: SharedPreferences

    override fun onFragmentInteraction(uri: Uri) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation_drawer)

        /*if(user != in project) //TODO
        nav_view.menu.findItem(R.id.nav_management_section).isVisible = false*/

        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(
            this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)

        accountSharedPref = getSharedPreferences(PREFERENCE_ACCOUNT, Context.MODE_PRIVATE)
        val accName = accountSharedPref.getString(AccountManager.KEY_ACCOUNT_NAME, "")
        if (accName.isNotBlank()) {
            nav_view.getHeaderView(0).findViewById<TextView>(R.id.accountEmail).text = accName
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
            /*    R.id.nav_share -> {

                }*/
            R.id.nav_manage -> {

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
