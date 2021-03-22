package com.h2play.canvas_magic.injection.component;

import dagger.Subcomponent;
import com.h2play.canvas_magic.injection.PerFragment;
import com.h2play.canvas_magic.injection.module.FragmentModule;

/**
 * This component inject dependencies to all Fragments across the application
 */
@PerFragment
@Subcomponent(modules = FragmentModule.class)
public interface FragmentComponent {
}
