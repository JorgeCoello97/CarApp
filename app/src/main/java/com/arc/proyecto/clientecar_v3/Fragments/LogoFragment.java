package com.arc.proyecto.clientecar_v3.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.arc.proyecto.clientecar_v3.R;
import com.skyfishjy.library.RippleBackground;

public class LogoFragment extends Fragment {
    public static LogoFragment newInstance() {
        
        Bundle args = new Bundle();
        
        LogoFragment fragment = new LogoFragment();
        fragment.setArguments(args);
        return fragment;
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
         View view = inflater.inflate(R.layout.fragment_logo,container,false);
        final RippleBackground rippleBackground = (RippleBackground) view.findViewById(R.id.content);
        ImageView imageView = (ImageView) view.findViewById(R.id.centerImage);
        rippleBackground.startRippleAnimation();
        imageView.startAnimation(
                AnimationUtils.loadAnimation(this.getContext(),R.anim.rotate_indefinitely)
        );
         return view;
    }
}
