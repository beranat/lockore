/*
 * Copyright (C) 2017 Anatoly madRat L. Berenblit <beranat@users.noreply.github.com>
 *
 * This file is part of Lockore application.
 *
 * Lockoree is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * Lockore distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Lockore.
 * If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.Vector;
import javax.microedition.lcdui.AlertType;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Item;

import madrat.gui.GenericItem;
import madrat.sys.QuickSort;
import madrat.i18n.I18N;
import madrat.storage.BinaryField;
import madrat.storage.Field;
import madrat.storage.IntegerField;
import madrat.storage.NobodyException;
import madrat.storage.Record;
import madrat.storage.Storage;
import madrat.storage.StringField;

/**
 * Add new or Edit existing record
 */
final class RecordEditScreen extends LockoreForm implements TaskNotification {
    private static final int DELETE = 1;
    private static final int UNSAVED = 2;

    protected final Record record_;
    protected final boolean isNewRecord_;

    protected final GenericItem name_;
    protected final GenericItem desc_;

    protected final Command edit_, add_, save_;

    protected final Command changeIcon_, renameField_, changeType_, delete_, protection_;

    protected final Command addFile_;

    public RecordEditScreen(LockoreForm owner, Record record) {
        super(null, owner, Command.CANCEL, HelpIndexList.RECORDS);

        add_ = new Command(I18N.get("New"), I18N.get("Add field"), Command.SCREEN, 1);
        edit_ = new Command(I18N.get("Edit"), Command.ITEM, 1);

        changeIcon_ = new Command(I18N.get("Icon"), I18N.get("Change icon"), Command.ITEM, 2);
        protection_ = new Command(I18N.get("Protection"), I18N.get("Toggle protection"), Command.ITEM, 3);

        changeType_ = new Command(I18N.get("Type"), I18N.get("Change type"), Command.ITEM, 4);
        renameField_= new Command(I18N.get("Rename"), I18N.get("Rename field"), Command.ITEM, 5);
        delete_     = new Command(I18N.get("Delete"), I18N.get("Delete field"), Command.ITEM, 6);

        addFile_ = new Command(I18N.get("Add file"), I18N.get("Load from file"), Command.SCREEN, 2);

        String apply, header;

        isNewRecord_ = (null == record);

        if (isNewRecord_) {
            apply = "Create";
            header = I18N.get("New Record");
            record_ = Midlet.getStore().newRecord();
            record_.setName(I18N.get("Unnamed"));
            record_.setIcon(Midlet.ICON_PASSWORD);
            record_.setDescription("");
            addNewField(true);
            record_.clearModify();
        }
        else {
            apply = "Save";
            header = record.getName();
            record_ = record;
        }

        if (record_.isNobody())
            throw new NobodyException();

        setTitle(header);
        name_ = createTitle(record_.getName(), record_.getIcon(), edit_);
        name_.setUserInfo(record_);
        name_.addCommand(changeIcon_);

        desc_ = createItem( I18N.get("Description"),
                            record_.getDescription(), Midlet.ICON_NULL, edit_);
        desc_.setUserInfo(record_);
        desc_.addCommand(changeIcon_);

        save_ = new Command(I18N.get(apply), Command.OK, 1);
        addCommand(save_);
        addCommand(add_);

        if (Midlet.isSupportFileApi())
            addCommand(addFile_);

        append(name_);
        append(desc_);

        updateUI(null);
    }

    public void updateUI(Object hint) {
        name_.setIcon(Midlet.getIconName(record_.getIcon()));
        name_.setTitle(record_.getName());
        desc_.setDesc(record_.getDescription());

        //remove all execept name_ and desc_
        for (int ind = size()-1; ind >=0; --ind) {
            final Item i = super.get(ind);
            if (i != name_ && i != desc_)
                delete(ind);
        }

        final int count = record_.size();
        if (0 == count)
            return;

        Vector v = new Vector(count);
        for (int i =0; i < count; ++i) {
            GenericItem item = createField(record_.at(i), edit_);
            item.addCommand(renameField_);
            item.addCommand(delete_);
            item.addCommand(protection_);
            item.addCommand(changeType_);
            item.addCommand(changeIcon_);
            v.addElement(item);
        }

        QuickSort.sort(v, new FieldSort());
        append(v);
    }

    protected void commandAction(Command c, GenericItem i) throws Exception {
        final Field field = (Field)i.getUserInfo();

        if (null == field)
            return;

        if (changeIcon_ == c) {
            Midlet.setCurrent(new SelectImageList(this, field));
            return;
        }

        if (edit_ == c) {
            Displayable editScreen;
            if (i == name_)
                editScreen = new ValueScreen(this, I18N.get("Record Name"), field, ValueScreen.NAME);
            else if (i == desc_)
                editScreen = new ValueScreen(this, null, field, ValueScreen.DESC);
            else if (Field.INTEGER == field.getType() && Midlet.DATE_FORMAT == field.getFormat())
                editScreen = new DateScreen(this, (IntegerField)field);
            else
                editScreen = new ValueScreen(this, null, field, ValueScreen.DATA);
            Midlet.setCurrent(editScreen);
            return;
        }

        if (renameField_ == c) {
            Midlet.setCurrent(new ValueScreen(this, I18N.get("Field Name"), field, ValueScreen.NAME));
            return;
        }

        if (delete_ == c) {
            ConfirmAlert.showConfirmOkCancel(this, I18N.get("Delete '{0}'?", field.getName()), "Yes", DELETE, i);
            return;
        }

        if (protection_ == c) {
            field.setProtection(!field.isProtected());
            updateUI(field);
            return;
        }

        if (changeType_ == c) {
            Midlet.setCurrent(new ChangeTypeList(this, field, i));
            return;
        }

        super.commandAction(c, i);
    }

    public void commandAction(Command c, Displayable d) {
        try {
            if (c == backCommand_) {
                if (!record_.isModified()) {
                    back_.show();
                    return;
                }
                ConfirmAlert.showConfirmYesNoBack(this, I18N.get("Save changes?"), UNSAVED, null);
                return;
            }
            if (add_ == c) {
                Field f = addNewField(false);
                updateUI(f);
                GenericItem i = findItem(f);
                if (null != i)
                    commandAction(renameField_, i);
            }
            else if (addFile_ == c) {
                Midlet.setCurrent(new AddFileList(this, record_));
            }
            else if (save_ == c) {
                final Storage store = Midlet.getStore();
                if (isNewRecord_)
                    store.insertNew(record_);
                else
                    store.updateExisting(record_);
                back_.show(record_);
            }
            else
                super.commandAction(c, d);
        }
        catch (Exception e) {
            Midlet.showAlert(e, this);
        }
    }

    public void onConfirmed(int action, Object userinfo, boolean isAgreed) throws Exception {
        if (action == UNSAVED) {
            if (isAgreed)
                commandAction(save_, this);
            else
                back_.show();
        }
        else if (action == DELETE) {
            GenericItem item = (GenericItem)userinfo;
            final Field field = (Field)item.getUserInfo();
            record_.remove(field);
            delete(item);
            show(null);
        }
        else
            super.onConfirmed(action, userinfo, isAgreed);
    }

    public void onFailure(Object id, Exception e) {
        Midlet.showAlert(e, this);
    }

    public void onSuccess(Object id, String unused0, long value, byte[] unused2) {
        final int type = (int)value;

        if (type == Field.NONE) {
            // cancel
            show();
            return;
        }

        try {
            final int format = (int)(value>>>32);
            final GenericItem item = (GenericItem)id;
            final Field field = (Field)item.getUserInfo();

            if (type == field.getType()) {
                field.setFormat(format);
                show(field);
                return;
            }

            final String name = field.getName();
            final int icon = field.getIcon();
            final boolean isProtected =  field.isProtected();

            Field nf = null;
            switch (type) {
                case Field.STRING:
                    nf = new StringField(isProtected, name, icon, format, field.getString());
                    break;
                case Field.INTEGER:
                    nf = new IntegerField(isProtected, name, icon, format, field.getLong());
                    break;
                case Field.BINARY:
                    BinaryField bf = new BinaryField(isProtected, name, icon, format, (byte[])null);
                    nf = bf;
                    if (field.getType() == Field.STRING)
                        bf.set(field.getString());
                    else {
                        byte[] b = Midlet.alloc(field.getBytes(null, 0));
                        field.getBytes(b, 0);
                        bf.assign(b);
                    }
                    break;
                default:
                    throw new IllegalArgumentException();
            }

            delete(item);
            record_.remove(field);
            record_.append(nf);
            show(nf);
        }
        catch (NumberFormatException e) {
            Midlet.showAlert(
                    AlertType.ERROR,
                    I18N.get("Inconceivable types convertion"),
                    null, this);
        }
        catch (IllegalArgumentException e) {
            Midlet.showAlert(
                    AlertType.ERROR,
                    I18N.get("Inconceivable types convertion"),
                    null, this);
        }
        catch(Exception e) {
            onFailure(id, e);
        }
    }

    private Field addNewField(boolean isPassword) {
        Field f = new StringField(false, I18N.get(isPassword?"Password":"Unnamed"),
                Midlet.ICON_NULL, Midlet.DEFAULT_FORMAT, "");
        f.setProtection(isPassword);
        record_.append(f);
        return f;
    }
}