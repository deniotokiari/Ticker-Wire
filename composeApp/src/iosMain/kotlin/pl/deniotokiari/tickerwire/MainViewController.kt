package pl.deniotokiari.tickerwire

import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import pl.deniotokiari.tickerwire.common.platform.StatusBarState
import pl.deniotokiari.tickerwire.feature.app.presentation.App
import platform.Foundation.NSCoder
import platform.Foundation.NSNotificationCenter
import platform.UIKit.NSLayoutConstraint
import platform.UIKit.UIStatusBarStyle
import platform.UIKit.UIStatusBarStyleDarkContent
import platform.UIKit.UIStatusBarStyleLightContent
import platform.UIKit.UIViewController
import platform.UIKit.addChildViewController
import platform.UIKit.didMoveToParentViewController

/**
 * Custom UIViewController that handles status bar style updates
 */
@OptIn(ExperimentalForeignApi::class)
class StatusBarAwareViewController : UIViewController {
    @OverrideInit
    constructor() : super(nibName = null, bundle = null)
    
    @OverrideInit
    constructor(coder: NSCoder) : super(coder)

    override fun viewDidLoad() {
        super.viewDidLoad()
        
        // Create the Compose view controller as a child
        val composeViewController = ComposeUIViewController { App() }
        addChildViewController(composeViewController)
        view.addSubview(composeViewController.view)
        composeViewController.view.translatesAutoresizingMaskIntoConstraints = false
        
        // Set up constraints
        NSLayoutConstraint.activateConstraints(listOf(
            composeViewController.view.topAnchor.constraintEqualToAnchor(view.topAnchor),
            composeViewController.view.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor),
            composeViewController.view.trailingAnchor.constraintEqualToAnchor(view.trailingAnchor),
            composeViewController.view.bottomAnchor.constraintEqualToAnchor(view.bottomAnchor)
        ))
        
        composeViewController.didMoveToParentViewController(this)
        
        // Observe status bar state changes
        NSNotificationCenter.defaultCenter.addObserver(
            observer = this,
            selector = platform.objc.sel_registerName("onStatusBarStateChanged"),
            name = StatusBarState.getNotificationName(),
            `object` = null
        )
        
        // Update status bar style based on current state
        updateStatusBarStyle()
    }
    
    override fun viewWillDisappear(animated: Boolean) {
        super.viewWillDisappear(animated)
        // Remove observer when view controller is deallocated
        NSNotificationCenter.defaultCenter.removeObserver(this)
    }
    
    override fun preferredStatusBarStyle(): UIStatusBarStyle {
        return if (StatusBarState.isDarkTheme) {
            UIStatusBarStyleLightContent
        } else {
            UIStatusBarStyleDarkContent
        }
    }
    
    override fun prefersStatusBarHidden(): Boolean = false
    
    private fun updateStatusBarStyle() {
        setNeedsStatusBarAppearanceUpdate()
    }
    
    @ObjCAction
    fun onStatusBarStateChanged() {
        updateStatusBarStyle()
    }
}

fun MainViewController(): UIViewController {
    return StatusBarAwareViewController()
}
