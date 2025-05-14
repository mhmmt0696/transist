package com.transist.ui.main

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.transist.ui.main.list.ListFragment
import com.transist.R
import com.transist.ui.main.study.random.StudyRandomFragment
import com.transist.ui.main.profile.ProfileFragment

class ActivityMain : AppCompatActivity() {

    private lateinit var viewModel: MainViewModel

    private lateinit var studyRandomFragment: Fragment
    private lateinit var listFragment: Fragment
    private lateinit var profileFragment: Fragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 1. Adım: Sistem padding’lerini devre dışı bırakıyorum.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.main_activity)

        val rootView = findViewById<ConstraintLayout>(R.id.main_root)
        // 2. Adım: Top bar ve bottom bar'a göre yeniden padding veriyorum.
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        // Fragmentler
        studyRandomFragment = StudyRandomFragment()
        listFragment = ListFragment()
        profileFragment = ProfileFragment()

        val transactionInitial = supportFragmentManager.beginTransaction()
            .add(R.id.fragment_container, studyRandomFragment, "studyRandom")
            .add(R.id.fragment_container, listFragment, "list")
            .add(R.id.fragment_container, profileFragment, "profile")
            .hide(listFragment)
            .hide(profileFragment)
            .hide(studyRandomFragment)

        transactionInitial.commit()

        // LiveData observer ile fragment değişimini yönet
        viewModel.selectedFragment.observe(this) { tag ->
            val transaction = supportFragmentManager.beginTransaction()
                //.hide(studyRandomFragment)
                .hide(listFragment)
                .hide(profileFragment)

            val editFragment = supportFragmentManager.findFragmentByTag("edit")
            if (editFragment != null) {
                transaction.remove(editFragment)
            }

            val addFragment = supportFragmentManager.findFragmentByTag("add")
            if (addFragment != null) {
                transaction.remove(addFragment)
            }

            val studyFolderFragment = supportFragmentManager.findFragmentByTag("studyFolder")
            if (studyFolderFragment != null) {
                transaction.hide(studyFolderFragment)
            }

            when (tag) {
                "studyRandom" -> transaction.show(studyRandomFragment)
                "list" -> transaction.show(listFragment)
                "profile" -> transaction.show(profileFragment)
            }
            transaction.commit()
        }

        // Başlangıç fragmentini yükle
        viewModel.loadInitialFragment()

        // Bottom nav click listener
        findViewById<ImageButton>(R.id.bn_home).setOnClickListener { viewModel.onBottomNavClicked("studyRandom") }
        findViewById<ImageButton>(R.id.bn_list).setOnClickListener { viewModel.onBottomNavClicked("list") }
        findViewById<ImageButton>(R.id.bn_profile).setOnClickListener { viewModel.onBottomNavClicked("profile") }


    }
}
