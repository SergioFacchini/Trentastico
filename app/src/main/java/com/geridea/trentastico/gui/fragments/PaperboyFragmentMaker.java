package com.geridea.trentastico.gui.fragments;


/*
 * Created with ♥ by Slava on 22/04/2017.
 */

import android.content.Context;
import android.support.annotation.NonNull;

import com.geridea.trentastico.R;
import com.github.porokoro.paperboy.DefaultItemTypes;
import com.github.porokoro.paperboy.ItemType;
import com.github.porokoro.paperboy.PaperboyFragment;
import com.github.porokoro.paperboy.ViewTypes;
import com.github.porokoro.paperboy.builders.ItemTypeBuilder;
import com.github.porokoro.paperboy.builders.ItemTypeBuilderKt;
import com.github.porokoro.paperboy.builders.PaperboyBuilder;
import com.github.porokoro.paperboy.builders.PaperboyBuilderKt;

import java.util.ArrayList;
import java.util.List;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public final class PaperboyFragmentMaker {

    private PaperboyFragmentMaker() { }

    @NonNull
    public static PaperboyFragment buildPaperboyFragment(final Context context) {
        return PaperboyBuilderKt.buildPaperboy(context, new Function1<PaperboyBuilder, Unit>() {
            @Override
            public Unit invoke(PaperboyBuilder paperboyBuilder) {
                List<ItemType> featureTypes = new ArrayList<>();

                featureTypes.add(ItemTypeBuilderKt.buildItemType(
                        context, DefaultItemTypes.INSTANCE.getIMPROVEMENT(), "Miglioramenti", "E",
                        new Function1<ItemTypeBuilder, Unit>() {
                            @Override
                            public Unit invoke(ItemTypeBuilder itemTypeBuilder) {
                                itemTypeBuilder.setColorRes(R.color.changelog_improvement);
                                itemTypeBuilder.setTitleSingular("Miglioramento");
                                itemTypeBuilder.setTitlePlural("Miglioramenti");
                                itemTypeBuilder.setIcon(R.drawable.ic_sentiment_satisfied_black_24dp);
                                itemTypeBuilder.setSortOrder(3);
                                return null;
                            }
                        }));


                featureTypes.add(ItemTypeBuilderKt.buildItemType(
                        context, DefaultItemTypes.INSTANCE.getBUG(), "Correzioni", "B",
                    new Function1<ItemTypeBuilder, Unit>() {
                        @Override
                        public Unit invoke(ItemTypeBuilder itemTypeBuilder) {
                            itemTypeBuilder.setColorRes(R.color.changelog_bug_fixed);
                            itemTypeBuilder.setTitleSingular("Correzione");
                            itemTypeBuilder.setTitlePlural("Correzioni");
                            itemTypeBuilder.setIcon(R.drawable.ic_mood_black_24dp);
                            itemTypeBuilder.setSortOrder(2);
                            return null;
                        }
                    }));

                featureTypes.add(ItemTypeBuilderKt.buildItemType(
                        context, DefaultItemTypes.INSTANCE.getFEATURE(), "Novità", "F",
                    new Function1<ItemTypeBuilder, Unit>() {
                        @Override
                        public Unit invoke(ItemTypeBuilder itemTypeBuilder) {
                            itemTypeBuilder.setColorRes(R.color.changelog_new_feature);
                            itemTypeBuilder.setTitleSingular("Novità");
                            itemTypeBuilder.setTitlePlural("Novità");
                            itemTypeBuilder.setIcon(R.drawable.ic_sentiment_very_satisfied_black_24dp);
                            itemTypeBuilder.setSortOrder(1);
                            return null;
                        }
                    }));

                paperboyBuilder.setViewType(ViewTypes.ICON);
                paperboyBuilder.setSectionLayout(R.layout.view_paperboy_version);
                paperboyBuilder.setItemLayout(R.layout.view_paperboy_item);
                paperboyBuilder.setSortItems(true);
                paperboyBuilder.setItemTypes(featureTypes);
                return null;
            }
        });
    }
}
