//
//  SettingsVC.swift
//  HouseParti
//
//  Created by Developer on 21/08/18.
//  Copyright © 2018 developer. All rights reserved.
//

import UIKit

class SettingsVC: UIViewController {
    
    var names = ["ABOUT": ["Terms of Use", "Privacy Policy"], "ACCOUNT": ["Reset Password", "Log out","Delete Account"]]
    
    struct Objects {
        var sectionName : String!
        var sectionObjects : [String]!
    }
    var objectArray = [Objects]()
    @IBOutlet weak var tbleViewSettings: UITableView!
    override func viewDidLoad() {
        super.viewDidLoad()
        self.title = "Settings"
        for (key, value) in names {
            objectArray.append(Objects(sectionName: key, sectionObjects: value))
        }
        
    }
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
}
//Mark:-UITableView Delegate and Datasouce
extension SettingsVC:UITableViewDelegate,UITableViewDataSource{
    
    func tableView(_ tableView: UITableView, heightForHeaderInSection section: Int) -> CGFloat {
        return 40
    }
    func tableView(_ tableView: UITableView, viewForHeaderInSection section: Int) -> UIView? {
        
        let headerView = UIView(frame: CGRect(x: 0, y:10, width: tableView.frame.size.width, height: 30))
        let menuHeaderLabel = UILabel(frame: CGRect(x: 22, y: 10, width: tableView.frame.size.width, height: 30))
        menuHeaderLabel.font = UIFont(name:"WhitneyHTF-SemiBold", size: 17.0)
        menuHeaderLabel.text = objectArray[section].sectionName
        menuHeaderLabel.textColor  = .white
        headerView.backgroundColor = .red
        headerView.addSubview(menuHeaderLabel)
        
        return headerView
    }
    func numberOfSections(in tableView: UITableView) -> Int {
        
       return objectArray.count
    }
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        
        return objectArray[section].sectionObjects.count
    }
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        
        let cell = tableView.dequeueReusableCell(withIdentifier: "HPMusicCell") as! HPMusicCell
        cell.musicLbl.text = objectArray[indexPath.section].sectionObjects[indexPath.row]
        cell.tintColor = UIColor.white
        
        if cell.musicLbl.text == "Delete Account"
        {
         cell.musicLbl.textColor = UIColor.red
        }
        return cell
    }
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath as IndexPath, animated: true)
         let getData = objectArray[indexPath.section].sectionObjects[indexPath.row]
        switch getData {
        case "Terms of Use":
            
            break
        case "":

            break
        case "Privacy Policy":
            
            break
        case "Reset Password":
            let cont = self.storyboard?.instantiateViewController(withIdentifier: "ResetPasswordVC") as! ResetPasswordVC
            self.navigationController?.pushViewController(cont, animated: true)
            break
        case "Log out":
            showLogoutMessage()

            break
        case "Delete Account":
            
            let alert = UIAlertController(title:"", message: NSLocalizedString("Are you sure you want delete account", comment:""), preferredStyle: .alert)
            alert.addAction(UIAlertAction(title: NSLocalizedString("Yes", comment:""), style: .default, handler: {  (_) in
                let parameters:[String: Any] = ["user_id": HPExtensions.shared.userId]
                UIApplication.shared.isNetworkActivityIndicatorVisible = true
                ConnectionManager.shared.postRequest(methodName:deleteAccount, parameters: parameters, onCompletion: { (response, error) in
                    if((error == nil)) {
                        let status = response["status"] as! Int
                        if status == 1 {
                            UIApplication.shared.isNetworkActivityIndicatorVisible = false
                            
                            let alertView = UIAlertController(title: "", message: response["message"] as? String, preferredStyle: .alert)
                            alertView.addAction(UIAlertAction(title: "OK", style: .default, handler: { (alertAction) -> Void in
                                let nav = self.storyboard?.instantiateViewController(withIdentifier: "nav") as! UINavigationController
                                UIApplication.shared.keyWindow?.rootViewController = nav
                            }))
                            self.present(alertView, animated: true, completion: nil)
                            
                            
                        } else {
                            self.showAlertWithMesssage(message: response["message"] as! String, VC: self)
                            UIApplication.shared.isNetworkActivityIndicatorVisible = false
                            return
                        }
                    } else {
                        self.showAlertWithMesssage(message: (error?.localizedDescription)!, VC: self)
                        UIApplication.shared.isNetworkActivityIndicatorVisible = false
                        return
                    }
                })
                
            }))
            alert.addAction(UIAlertAction(title: NSLocalizedString("NO", comment:""), style: .default, handler: {  (_) in
            }))
            self.present(alert, animated: true, completion: nil)

            break
        default:
            break
        }
        
    }
    func showLogoutMessage() {
        let alert = UIAlertController(title: title, message: NSLocalizedString("Are you sure you want to log out?", comment:""), preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: NSLocalizedString("Log out", comment:""), style: .default, handler: {  (_) in
            HPExtensions.shared.userId = ""
            HPExtensions.shared.loginStatus = false
            let nav = self.storyboard?.instantiateViewController(withIdentifier: "nav") as! UINavigationController
            UIApplication.shared.keyWindow?.rootViewController = nav
        }))
        alert.addAction(UIAlertAction(title: NSLocalizedString("Cancel", comment:""), style: .default, handler: {  (_) in
            
        }))
        self.present(alert, animated: true, completion: nil)
    }
    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat{
        return 50
    }
    func tableView(_ tableView: UITableView, estimatedHeightForRowAt indexPath: IndexPath) -> CGFloat {
        return UITableViewAutomaticDimension
    }
    
}
